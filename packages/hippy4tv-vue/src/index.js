import registerWaterfall from './waterfall';

/**
 * Register the swiper component.
 */
const WaterfallComponent = {
  install(Vue) {
    registerWaterfall(Vue);
  },
};

/**
 * Register all of native components
 */
const Hippy4tvNativeComponents = {
  install(Vue) {
    registerWaterfall(Vue);
  },
};

export default Hippy4tvNativeComponents;
// Export specific component for TreeSharking.
export {
  WaterfallComponent,
};
