import { spawn } from 'child_process';
import { readFileSync } from 'fs';
import { argv } from 'process';

const groupId = argv[2];
const artifactId = argv[3];
const product = argv[4];

const productWithVersions = {
  jira: [ 
    '9.0.0', 
    '9.1.0', 
    '9.2.0', 
    '9.3.0', 
    '9.4.0', 
    '9.5.0', 
    '9.6.0', 
    '9.7.0', 
    '9.8.0', 
    '9.9.0', 
    '9.10.0', 
    '9.11.0', 
    '9.12.0', 
    '9.13.0', 
    '9.14.0', 
    '9.15.0', 
    '9.16.0', 
    '9.17.0', 
    '10.0.0',
    '10.1.1',
    '10.2.0'
  ],
  confluence: [
    '8.0.0',
    '8.1.0',
    '8.2.0',
    '8.3.0',
    '8.4.0',
    '8.5.0',
    '8.6.0',
    '8.7.1',
    '8.8.0',
    '8.9.0',
    '9.0.1',
    '9.1.0',
    '9.2.0',
    '9.3.1',
    '9.3.2',
    '9.4.0',
    '9.4.1',
    '9.5.1',
    '9.5.2',
    '9.5.3',
    '10.0.1',
    '10.0.2'
  ],
  bitbucket: [
    '8.0.0',
    '8.1.0',
    '8.2.0',
    '8.3.0',
    '8.4.0',
    '8.5.0',
    '8.6.0',
    '8.7.0',
    '8.8.0',
    '8.9.0',
    '8.10.0',
    '8.11.0',
    '8.12.0',
    '8.13.0',
    '8.14.0',
    '8.15.0',
    '8.16.0',
    '8.17.0',
    '8.18.0',
    '8.19.0',
    '9.0.0',
    '9.1.0',
    '9.2.0',
    '9.3.0',
    '9.4.0',
    '9.5.0'
  ],
  bamboo: [
    '9.0.0',
    '9.1.0',
    '9.2.0',
    '9.3.0',
    '9.4.0',
    '9.5.0',
    '9.6.0',
    '10.0.0',
    '10.1.0'
  ]
}

const output = {};

const main = async () => {

  const includedProducts = product ? Object.entries(productWithVersions).filter(item => item[0] === product) : Object.entries(productWithVersions);

  for await (const [ product, versions ] of includedProducts) {

    for await (const version of versions) {
      await new Promise((resolve, reject) => {
        const mvn = spawn(
          'mvn',
          [
            '-P', `servlet`,
            'dependency:tree',
            `-DoutputType=json`,
            '-DoutputFile=dependencies.json',
            `-D${product}.version=${version}`
          ],
          { stdio: 'inherit' }
        );
        mvn.on('exit', (code) => (code === 0) ? resolve() : reject(new Error(`Maven exited with code ${code}`)));
      });

      const content = readFileSync('./dependencies.json', 'utf-8');
      const dependencies = JSON.parse(content);

      const children = dependencies.children || [];
      const dependency = children.find(item => item.groupId === groupId && item.artifactId === artifactId);
      if (dependency) {
        output[product] = output[product] || {};
        output[product][dependency.version] = output[product][dependency.version] || [];
        output[product][dependency.version].push(version);
      }
    }
  
  }

  const result = product ? `
<!--
    ${product}
${Object.entries(output[product]).map(([ version, productVersions ]) => `    ${version} - ${product} ${productVersions[0]} - ${productVersions[productVersions.length - 1]}\n`)}
-->
` : `
<!--
    Platform 6

    Platform 7

    Jira
${Object.entries(output['jira']).map(([ version, productVersions ]) => `    ${version} - Jira ${productVersions[0]} - ${productVersions[productVersions.length - 1]}\n`)}
    Confluence
${Object.entries(output['confluence']).map(([ version, productVersions ]) => `    ${version} - Confluence ${productVersions[0]} - ${productVersions[productVersions.length - 1]}\n`)}
    Bitbucket
${Object.entries(output['bitbucket']).map(([ version, productVersions ]) => `    ${version} - Bitbucket ${productVersions[0]} - ${productVersions[productVersions.length - 1]}\n`)}
    Bamboo
${Object.entries(output['bamboo']).map(([ version, productVersions ]) => `    ${version} - Bamboo ${productVersions[0]} - ${productVersions[productVersions.length - 1]}\n`)}
-->`;

  console.log(result.replaceAll(',    ', '    '));
}

main();