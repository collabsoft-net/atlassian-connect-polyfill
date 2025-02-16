import { getMetaData } from '@collabsoft-net/connect';
import { Host } from '@collabsoft-net/connect/dist/es2020/Host';

import { getAppUrl } from './helpers/getAppUrl';

interface WindowWithAC extends Window {
  BitbucketExampleHost?: Host;
}

try {
  // Remove footer to emulate Connect
  document.documentElement.classList.add('page-type-connect');

  // Set the app key, either from environment variable or fallback
  const appKey = process.env.APPKEY || 'fyi.iapetus.examples.bitbucket-example';

  // We need to make sure we only initialize AC polyfill once
  // After initialisation, the AC object is placed on the window object
  // If it's already there, don't bother, otherwise go for it!
  const windowWithAC = window as WindowWithAC;
  if (!windowWithAC.BitbucketExampleHost) {
    windowWithAC.BitbucketExampleHost = new Host({
      appKey,
      product: 'bitbucket',
      baseUrl: getAppUrl('/plugins/servlet/bitbucket-example', true),
      xdm_e: getMetaData('base-url') || getMetaData('bitbucket-base-url') || '',
      contextPath: getMetaData('context-path') || getMetaData('bitbucket-context-path') || '',
      license: getMetaData(`${appKey}-lic`) === 'active' ? 'active' : 'none',
      verbose: true
    });
    windowWithAC.BitbucketExampleHost.init();
  }
} catch (err) {
  console.error('[AC] Failed to initialize Atlassian Connect polyfill', err);
}