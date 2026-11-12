// The bridge from Kotlin to the vision model (Describe, Find, Ask): one model held open, one
// picture and one prompt in, the answer's text out. Everything that decides what is shown to the
// model and what of its answer is spoken lives in engine/see, where it is checked; this file only
// carries bytes and must never hold a decision.
//
// Runs on llama.cpp's multimodal library (MIT, pinned as a submodule in third_party/llama.cpp).

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define TAG "IkshanaVision"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

struct Vision {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    mtmd_context *mtmd = nullptr;
    llama_sampler *sampler = nullptr;
    int n_batch = 512;
};

void close(Vision *v) {
    if (!v) return;
    if (v->sampler) llama_sampler_free(v->sampler);
    if (v->mtmd) mtmd_free(v->mtmd);
    if (v->ctx) llama_free(v->ctx);
    if (v->model) llama_model_free(v->model);
    delete v;
}

std::string str(JNIEnv *env, jstring s) {
    const char *c = env->GetStringUTFChars(s, nullptr);
    std::string out(c);
    env->ReleaseStringUTFChars(s, c);
    return out;
}

// A model can stop mid-character (a multi-byte letter split across two tokens, the commonest case
// in Malayalam). Cut back to the last whole character so no broken letter reaches the voice.
std::string whole_utf8(std::string s) {
    size_t i = s.size(), back = 0;
    while (i > 0 && back < 4) {
        unsigned char c = s[i - 1];
        if ((c & 0xC0) != 0x80) {  // a lead or ASCII byte
            int need = c < 0x80 ? 1 : (c >> 5) == 6 ? 2 : (c >> 4) == 14 ? 3 : (c >> 3) == 30 ? 4 : 0;
            if (need != (int) back + 1) s.resize(i - 1);
            break;
        }
        --i; ++back;
    }
    return s;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_layerbit_ikshana_see_VisionModel_nativeOpen(JNIEnv *env, jclass, jstring jModel, jstring jProj, jint threads) {
    llama_backend_init();
    auto *v = new Vision();
    auto mp = llama_model_default_params();
    v->model = llama_model_load_from_file(str(env, jModel).c_str(), mp);
    if (!v->model) { LOGE("model did not load"); close(v); return 0; }

    auto cp = llama_context_default_params();
    cp.n_ctx = 4096;              // an image is up to ~1000 tokens on these models; the answer is short
    cp.n_batch = v->n_batch;
    cp.n_threads = threads;
    cp.n_threads_batch = threads;
    v->ctx = llama_init_from_model(v->model, cp);
    if (!v->ctx) { LOGE("context did not open"); close(v); return 0; }

    auto vp = mtmd_context_params_default();
    vp.use_gpu = false;           // CPU first: measured on the moto before any GPU path is trusted
    vp.n_threads = threads;
    vp.warmup = false;
    v->mtmd = mtmd_init_from_file(str(env, jProj).c_str(), v->model, vp);
    if (!v->mtmd) { LOGE("projector did not load"); close(v); return 0; }

    // Greedy: the same picture gets the same words, which is what a listener checking twice expects.
    v->sampler = llama_sampler_init_greedy();
    LOGI("open, %d threads", threads);
    return reinterpret_cast<jlong>(v);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_layerbit_ikshana_see_VisionModel_nativeAsk(JNIEnv *env, jclass, jlong handle, jbyteArray jRgb,
                                                   jint width, jint height, jstring jPrompt, jint maxTokens) {
    auto *v = reinterpret_cast<Vision *>(handle);
    if (!v) return nullptr;

    std::vector<unsigned char> rgb(env->GetArrayLength(jRgb));
    env->GetByteArrayRegion(jRgb, 0, (jsize) rgb.size(), reinterpret_cast<jbyte *>(rgb.data()));
    if (rgb.size() != (size_t) width * height * 3) { LOGE("rgb size %zu for %dx%d", rgb.size(), width, height); return nullptr; }
    mtmd_bitmap *bitmap = mtmd_bitmap_init((uint32_t) width, (uint32_t) height, rgb.data());

    // The model's own chat template around one user turn: the picture's marker, then the prompt.
    std::string user = std::string(mtmd_default_marker()) + "\n" + str(env, jPrompt);
    llama_chat_message msg{"user", user.c_str()};
    const char *tmpl = llama_model_chat_template(v->model, nullptr);
    std::vector<char> buf(user.size() * 2 + 512);
    int n = llama_chat_apply_template(tmpl, &msg, 1, true, buf.data(), (int32_t) buf.size());
    if (n > (int) buf.size()) { buf.resize(n); n = llama_chat_apply_template(tmpl, &msg, 1, true, buf.data(), n); }
    std::string prompt = n > 0 ? std::string(buf.data(), n) : user;

    mtmd_input_chunks *chunks = mtmd_input_chunks_init();
    mtmd_input_text text{prompt.c_str(), prompt.size(), true, true};
    const mtmd_bitmap *bitmaps[] = {bitmap};
    int32_t res = mtmd_tokenize(v->mtmd, chunks, &text, bitmaps, 1);
    mtmd_bitmap_free(bitmap);
    if (res != 0) { LOGE("tokenize %d", res); mtmd_input_chunks_free(chunks); return nullptr; }

    llama_memory_clear(llama_get_memory(v->ctx), true);  // every question starts from a clean slate
    llama_sampler_reset(v->sampler);
    llama_pos n_past = 0;
    res = mtmd_helper_eval_chunks(v->mtmd, v->ctx, chunks, 0, 0, v->n_batch, true, &n_past);
    mtmd_input_chunks_free(chunks);
    if (res != 0) { LOGE("eval %d", res); return nullptr; }

    const llama_vocab *vocab = llama_model_get_vocab(v->model);
    std::string out;
    llama_batch batch = llama_batch_init(1, 0, 1);
    for (int i = 0; i < maxTokens; ++i) {
        llama_token tok = llama_sampler_sample(v->sampler, v->ctx, -1);
        if (llama_vocab_is_eog(vocab, tok)) break;
        char piece[256];
        int len = llama_token_to_piece(vocab, tok, piece, sizeof piece, 0, false);
        if (len > 0) out.append(piece, len);
        batch.n_tokens = 1;
        batch.token[0] = tok;
        batch.pos[0] = n_past++;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        if (llama_decode(v->ctx, batch) != 0) { LOGE("decode at %d", i); break; }
    }
    llama_batch_free(batch);
    // Bytes, not NewStringUTF: that takes modified UTF-8 and mangles a four-byte character.
    std::string whole = whole_utf8(out);
    jbyteArray result = env->NewByteArray((jsize) whole.size());
    env->SetByteArrayRegion(result, 0, (jsize) whole.size(), reinterpret_cast<const jbyte *>(whole.data()));
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_layerbit_ikshana_see_VisionModel_nativeClose(JNIEnv *, jclass, jlong handle) {
    close(reinterpret_cast<Vision *>(handle));
}
