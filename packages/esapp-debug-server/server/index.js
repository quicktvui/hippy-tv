const fs = require('fs');
const Koa = require('koa');
const path = require('path');
const devSupportWsServer = require('./websocketProxy');
const liveReloadWsServer = require('./hippy-livereload');
const {
  getFrameworkVersion,
  logger,
  exec,
  content,
  parseMimeType,
} = require('../utils');

async function startDevServer(args) {
  const {
    static,
    entry = 'dist/dev/index.bundle',
    host = '127.0.0.1',
    port = 38989,
    verbose,
  } = args;

  if (!getFrameworkVersion()) {
    throw new Error('The current folder is not containing hippy4tv project.');
  }

  const versionReturn = '{"Browser": "Hippy App/v1.0.0","Protocol-Version": "1.1"}';
  const jsonReturn = JSON.stringify([{
    description: 'Hippy instance',
    devtoolsFrontendUrl: `chrome-devtools://devtools/bundled/js_app.html?experiments=true&v8only=true&ws=${host}:${port}/debugger-proxy?role=chrome`,
    devtoolsFrontendUrlCompat: `chrome-devtools://devtools/bundled/inspector.html?experiments=true&v8only=true&ws=${host}:${port}/debugger-proxy?role=chrome`,
    faviconUrl: 'http://res.imtt.qq.com/hippydoc/img/hippy-logo.ico',
    title: 'Hippy debug tools for V8',
    type: 'node',
    url: '',
    webSocketDebuggerUrl: `ws://${host}:${port}/debugger-proxy?role=chrome`,
  }]);

  const app = new Koa();

  let staticPath;
  if (static) {
    staticPath = path.resolve(static);
  } else {
    staticPath = path.resolve(path.dirname(entry));
  }

  if (!fs.statSync(staticPath).isDirectory()) {
    throw new Error('Static folder is not exist or not a folder');
  }

  app.use((ctx) => {
    if (verbose) {
      logger.info('Received url request', ctx.url);
    }
    // Response entry js file for debugging
    if (ctx.path === '/index.bundle') {
      if (verbose) {
        logger.info('Reading entry', entry);
      }
      const jsContent = fs.readFileSync(entry);
      ctx.res.writeHead(200, {
        'Content-Type': 'application/javascript',
      });
      ctx.res.write(jsContent);
      return ctx.res.end();
    }

    // Response the content of version request
    if (ctx.path === '/json/version') {
      ctx.res.writeHead(200, {
        'Content-Type': 'application/json; charset=UTF-8',
        'Content-Length': versionReturn.length,
      });
      ctx.res.write(versionReturn);
      return ctx.res.end();
    }

    // Response the content of json api request
    if (ctx.path === '/json') {
      ctx.res.writeHead(200, {
        'Content-Type': 'application/json; charset=UTF-8',
        'Content-Length': jsonReturn.length,
      });
      ctx.res.write(jsonReturn);
      return ctx.res.end();
    }

    let _mime = parseMimeType(ctx.path)
    if(_mime && _mime.indexOf('image/') >= 0){
      let _content = content(ctx, staticPath);
      ctx.res.writeHead(200)
      ctx.res.write(_content, 'binary')
      ctx.res.end()
      return
    }

    // Read the file content from specific path
    const contentStr = content(ctx, staticPath);
    ctx.res.writeHead(200, {
      'Content-Type': parseMimeType(ctx.path),
    });
    ctx.res.write(contentStr);
    return ctx.res.end();
  });

  const serverInstance = app.listen(port, host, () => {
    // exec('adb', ['reverse', '--remove-all'])
    //   .then(() => exec('adb', ['reverse', `tcp:${port}`, `tcp:${port}`]))
    //   .catch((err) => {
    //     logger.warn('Port reverse failed, For iOS app debug only just ignore the message.');
    //     logger.warn('Otherwise please check adb devices command working correctly');
    //     if (verbose) {
    //       logger.error(err);
    //     }
    //   });
    devSupportWsServer.startWebsocketProxyServer(serverInstance, '/debugger-proxy');
    liveReloadWsServer.startLiveReloadServer(serverInstance, '/debugger-live-reload');
    logger.info('Hippy debug server is started at', `${host}:${port}`, 'for', entry);
    logger.info('Please open "chrome://inspect" in Chrome to debug your android EsApp app, or use Safari to debug iOS app');
  });
}

module.exports = startDevServer;
