import { defineConfig } from "cypress";
import {addCucumberPreprocessorPlugin} from '@badeball/cypress-cucumber-preprocessor';
import createEsbuildPlugin from '@badeball/cypress-cucumber-preprocessor/esbuild';
import {clearDownloads} from './cypress/helper/file/clear-downloads.task';
import createBundler from '@bahmutov/cypress-esbuild-preprocessor';

export default defineConfig({
  component: {
    devServer: {
      framework: 'angular',
      bundler: 'webpack',
    },
    specPattern: '**/*.cy.ts',
  },

  e2e: {
    specPattern: 'cypress/**/*.feature',
    viewportWidth: 1920,
    viewportHeight: 1080,
    setupNodeEvents: async (on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions): Promise<Cypress.PluginConfigOptions> => {
      await addCucumberPreprocessorPlugin(on, config);
      on(
        'file:preprocessor',
        createBundler({
          plugins: [createEsbuildPlugin(config)],
        }),
      );
      on('task', {
        clearDownloads: () => {
          return clearDownloads(config.downloadsFolder);
        },
      });
      return config;
    },
  },
});
