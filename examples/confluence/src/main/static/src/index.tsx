import './styling.css';

import { loadAP, resizeFix } from '@collabsoft-net/connect';
import { isOfType } from '@collabsoft-net/helpers';
import React from 'react';
import ReactDOM from 'react-dom/client';

import { getAppUrl } from './helpers/getAppUrl';

(async () => {
  // Load AP from the Atlassian Connect Polyfill
  const ACJS = await loadAP({
    url: getAppUrl('/plugins/servlet/atlassian-connect/all.js', true),
    resize: false,
    sizeToParent: false,
    margin: false,
    base: false
  });

  // Add support for dark mode
  ACJS.theming.initializeTheming();

  // Make sure to disable automatic close on submit
  // This is to ensure that our onSubmit event handler is fired
  ACJS.dialog.disableCloseOnSubmit();

  // Check if we are running in Confluence
  if (isOfType<AP.ConfluenceInstance>(ACJS, 'confluence')) {
    // Check if we have a submit button!
    const submitButton = ACJS.dialog.getButton('submit');
    if (submitButton) {
      // Attach our onSubmit event handler
      // This saves the macro (required) and closes the editor
      submitButton.bind(() => {
        ACJS.confluence.saveMacro({});
        ACJS.confluence.closeMacroEditor();
      });
    }
  }

  // Retrieve the current host location
  const location = await new Promise<string>(resolve => ACJS.getLocation(resolve));

  // Render the content
  ReactDOM.createRoot(document.body).render(
    <div className='ac-content' style={{ padding: '0 20px 20px 20px' }}>
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

  // Make sure that the iframe is automatically resized based on content
  await resizeFix();
})();
