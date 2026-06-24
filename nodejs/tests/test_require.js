const serverModulePath = require.resolve('../src/server');

// Create a dummy module entry in the cache
require.cache[serverModulePath] = new (require('module'))(serverModulePath, module.parent);

// Set require.main to refer to this dummy module
const originalMain = require.main;
require.main = require.cache[serverModulePath];

const { server } = require("../src/server");
console.log("Success?");
