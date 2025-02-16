import { token } from '@atlaskit/tokens';
import { loadAP, resizeFix } from '@collabsoft-net/connect';
import React from 'react';
import ReactDOM from 'react-dom/client';

import { getAppUrl } from './helpers/getAppUrl';

(async () => {

  const ACJS = await loadAP({
    url: getAppUrl('/plugins/servlet/atlassian-connect/all.js', true),
    resize: false,
    sizeToParent: false,
    margin: false,
    base: false
  });

  // Add support for dark mode
  ACJS.theming.initializeTheming();

  const location = await new Promise<string>(resolve => ACJS.getLocation(resolve));

  ReactDOM.createRoot(document.body).render(
    <div className='ac-content' style={{ padding: '0 20px 20px 20px', color: token('color.text') }}>
      <h1>Atlassian Connect Polyfill</h1>
      <p>
        Host URL: <br />
        { location }
      </p>
      <p>
        Frame URL: <br />
        { window.location.href }
      </p>
    </div>
  );

  await resizeFix();
})();
