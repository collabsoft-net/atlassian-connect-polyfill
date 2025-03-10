import { getMetaData } from '@collabsoft-net/connect';
import { Host } from '@collabsoft-net/connect/dist/es2020/Host';

import { getAppUrl } from './helpers/getAppUrl';

interface WindowWithAC extends Window {
  BambooExampleHost?: Host;
}

try {
  // Remove footer to emulate Connect
  document.documentElement.classList.add('page-type-connect');

  // Set the app key, either from environment variable or fallback
  const appKey = process.env.APPKEY || 'fyi.iapetus.examples.bamboo-example';

  // We need to make sure we only initialize AC polyfill once
  // After initialisation, the AC object is placed on the window object
  // If it's already there, don't bother, otherwise go for it!
  const windowWithAC = window as WindowWithAC;
  if (!windowWithAC.BambooExampleHost) {
    windowWithAC.BambooExampleHost = new Host({
      appKey,
      product: 'bamboo',
      baseUrl: getAppUrl('/plugins/servlet/bamboo-example', true),
      xdm_e: getMetaData('base-url') || getMetaData('bamboo-base-url') || '',
      contextPath: getMetaData('context-path') || getMetaData('bamboo-context-path') || '',
      license: getMetaData(`${appKey}-lic`) === 'active' ? 'active' : 'none',
      verbose: true
    });
    windowWithAC.BambooExampleHost.init();
  }
} catch (err) {
  console.error('[AC] Failed to initialize Atlassian Connect polyfill', err);
}