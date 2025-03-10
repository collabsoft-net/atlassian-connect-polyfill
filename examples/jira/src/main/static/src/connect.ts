import { getMetaData } from '@collabsoft-net/connect';
import { Host } from '@collabsoft-net/connect/dist/es2020/Host';

import { getAppUrl } from './helpers/getAppUrl';

interface WindowWithAC extends Window {
  JiraExampleHost?: Host;
}

try {
  // Remove footer to emulate Connect
  const isFullPage = getMetaData('full-page') === 'true';
  if (isFullPage) {
    document.documentElement.classList.add('page-type-connect');
  }

  // Set the app key, either from environment variable or fallback
  const appKey = process.env.APPKEY || 'fyi.iapetus.examples.jira-example';

  // We need to make sure we only initialize AC polyfill once
  // After initialisation, the AC object is placed on the window object
  // If it's already there, don't bother, otherwise go for it!
  const windowWithAC = window as WindowWithAC;
  if (!windowWithAC.JiraExampleHost) {
    windowWithAC.JiraExampleHost = new Host({
      appKey,
      product: 'jira',
      baseUrl: getAppUrl('/plugins/servlet/jira-example', true),
      xdm_e: getMetaData('base-url') || getMetaData('jira-base-url') || '',
      contextPath: getMetaData('context-path') || getMetaData('jira-context-path') || '',
      license: getMetaData(`${appKey}-lic`) === 'active' ? 'active' : 'none',
      verbose: true
    });
    windowWithAC.JiraExampleHost.init();
  }
} catch (err) {
  console.error('[AC] Failed to initialize Atlassian Connect polyfill', err);
}