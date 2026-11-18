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
      const {env} = config;
      const stage = env.STAGES?.[env.STAGE];
      config.expose = {
        BASE_URL: stage?.BASE_URL ?? env.BASE_URL,
        KEYCLOAK_URL: stage?.KEYCLOAK_URL ?? env.KEYCLOAK_URL,
        KEYCLOAK_REALM: stage?.KEYCLOAK_REALM ?? env.KEYCLOAK_REALM,
        API_VERSION: env.API_VERSION,
        MIN_DOM: env.MIN_DOM,
        TIMEOUT_XS: env.TIMEOUT_XS,
        TIMEOUT_S: env.TIMEOUT_S,
        TIMEOUT_M: env.TIMEOUT_M,
        TIMEOUT_L: env.TIMEOUT_L,
        ...config.expose,
      };
      env.USERNAME = stage?.USERNAME ?? env.USERNAME;
      env.PASSWORD = stage?.PASSWORD ?? env.PASSWORD;

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
