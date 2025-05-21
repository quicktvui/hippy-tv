function registerWaterfall(Vue) {
  Vue.registerElement('Waterfall', {
    component: {
      name: 'Waterfall',
    },
  });

  Vue.component('waterfall', {
    props: {
      color: Number,
    },
    methods: {
      setWaterfallData(data) {
        Vue.Native.callUIFunction(this.$refs.Waterfall, 'setWaterfallData', data);
      },
    },
    render(h) {
      return h(
        'Waterfall',
        {
          ref: 'waterfall',
        }, this.$slots.default
      );
    },
  });

}

export default registerWaterfall;
