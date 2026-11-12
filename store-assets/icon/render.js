// Screenshots each job in jobs.json with Playwright's Chromium. Called by build_icon.py.
// A job is {svg, size, out, radius?, transparent?} or {html, width, height, out}.
const { chromium } = require('playwright');
const fs = require('fs'); const path = require('path');
(async () => {
  const jobs = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const here = path.dirname(process.argv[2]);
  const browser = await chromium.launch();
  for (const j of jobs) {
    fs.mkdirSync(path.dirname(j.out), { recursive: true });
    const page = await browser.newPage({ viewport: { width: j.width || j.size, height: j.height || j.size }, deviceScaleFactor: 1 });
    if (j.html) {
      await page.goto('file://' + path.join(here, j.html));
    } else {
      const svg = fs.readFileSync(path.join(here, j.svg), 'utf8');
      const r = j.radius ? `border-radius:${j.radius * 100}%;` : '';
      await page.setContent(`<html><body style="margin:0;background:transparent"><div style="width:${j.size}px;height:${j.size}px;overflow:hidden;${r}">${svg.replace('width="108" height="108"', `width="${j.size}" height="${j.size}"`)}</div></body></html>`);
    }
    await page.screenshot({ path: j.out, omitBackground: !!j.transparent });
    await page.close();
  }
  await browser.close();
})();
