
new MutationObserver((mutation, observer) => {
  const hasStyle = document.getElementById('ac-polyfill-css-reset') !== null;
  if (!hasStyle) {
    const contentPanel = document.querySelector('.page-type-connect .aui-page-panel-content');
    if (contentPanel) {
      const template = `
<style id="ac-polyfill-css-reset">
  :root {
    --content-panel-offset: ${contentPanel.getBoundingClientRect().top}px;
  }
</style>`
      const html = new DOMParser().parseFromString(template, 'text/html');
      const style = html.head.children[0];
      document.body.appendChild(style);
      observer.disconnect();
    }
  } else {
    observer.disconnect();
  }
}).observe(document.documentElement, { attributes: true, childList: true, subtree: true });