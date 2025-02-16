/* eslint-disable */

const path = require('path');
const webpack = require('webpack');
const TerserPlugin = require('terser-webpack-plugin');
const WebResourcePlugin = require('atlassian-webresource-webpack-plugin');

module.exports = {

  mode: process.env.NODE_ENV === 'production' ? 'production' : 'development',

  entry: {
    app: './src/index.tsx',
    connect: './src/connect.ts'
  },

  output: {
    filename: `[name].[fullhash].js`,
    path: path.resolve('../../../', 'target', 'classes', 'static'),
    chunkFilename: `[name].[fullhash].js`
  },

  devtool: process.env.NODE_ENV === 'production' ? false : 'cheap-source-map',

  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    fallback: {
      crypto: require.resolve('crypto-browserify'),
      stream: require.resolve('web-streams-polyfill'),
      buffer: require.resolve('buffer'),
      url: require.resolve('url'),
      string_decoder: require.resolve('string_decoder'),
      assert: require.resolve('assert'),
      http: require.resolve('stream-http'),
      https: require.resolve('https-browserify'),
      os: require.resolve('os-browserify/browser'),
      zlib: require.resolve('browserify-zlib'),
      fs: false,
      dgram: false,
      net: false,
      tls: false,
      child_process: false,
      vm: false
    }
  },

  node: {
    // prevent webpack from injecting eval / new Function through global polyfill
    global: false
  },

  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        loader: 'ts-loader',
        exclude: [
          /\/node_modules\//
        ]
      },
      {
        test: /\.(jpe?g|gif|png|svg)$/i,
        use: [
          {
            loader: 'url-loader',
            options: {
              limit: 100000
            }
          }
        ]
      },
      {
        test: /\.css$/,
        use: [
          { loader: 'style-loader' },
          { loader: 'css-loader' }
        ]
      }
    ],
  },

  optimization: {
    minimize: true,
    usedExports: true,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          output: {
            comments: false,
          },
        },
        extractComments: false,
      }),
    ],
    runtimeChunk: 'single',
    splitChunks: {
      chunks: 'all',
      maxInitialRequests: Infinity,
      minSize: 0,
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
        },
      },
    },
  },

  plugins: [
    new webpack.ProvidePlugin({
      process: 'process/browser',
      global: 'core-js/es/global-this.js',
      Buffer: ['buffer', 'Buffer']
    }),
    new webpack.DefinePlugin({
      'gobal': 'window',
      'process.env.APPKEY': JSON.stringify(process.env.APPKEY),
      'process.env.NODE_ENV' : JSON.stringify(process.env.NODE_ENV === 'production' ? 'production' : 'development')
    }),
    new WebResourcePlugin({
      pluginKey: 'fyi.iapetus.examples.bitbucket-example',
      contextMap: {
        app: [ `bitbucket-example` ],
        connect: [ `bitbucket-example-atlassian-connect` ]
      },
      locationPrefix: 'static/',
      addEntrypointNameAsContext: false,
      xmlDescriptors: path.resolve('../../../', 'target', 'classes', 'META-INF', 'plugin-descriptors', 'webResources.xml'),
      transformationMap: false
    })
  ]
};
