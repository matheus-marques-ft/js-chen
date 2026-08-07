import Vue from 'vue'
import Vuex from 'vuex'
import getters from './getters'

Vue.use(Vuex)

// Automatically load modules from the modules directory
const MODULES_FILES = require.context('./modules', true, /\.js$/)

// The module name is the JS filename, e.g. user.js becomes the user module
const modules = MODULES_FILES.keys().reduce((modules, modulePath) => {
  const value = MODULES_FILES(modulePath)
  const moduleName = modulePath.replace(/^\.\/(.*)\.\w+$/, '$1')
  modules[moduleName] = value.default
  return modules
}, {})

const store = new Vuex.Store({
  modules,
  getters
})

export default store
