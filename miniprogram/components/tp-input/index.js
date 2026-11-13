Component({
  properties: {
    value: { type: String, value: '' },
    placeholder: { type: String, value: '请输入' },
    type: { type: String, value: 'text' },
    password: { type: Boolean, value: false },
    textarea: { type: Boolean, value: false },
    disabled: { type: Boolean, value: false },
    error: { type: String, value: '' },
    maxlength: { type: Number, value: 140 },
    rows: { type: Number, value: 3 }
  },
  data: { focused: false },
  methods: {
    onInput(e) {
      this.triggerEvent('input', { value: e.detail.value })
    },
    onFocus() {
      this.setData({ focused: true })
      this.triggerEvent('focus')
    },
    onBlur() {
      this.setData({ focused: false })
      this.triggerEvent('blur')
    }
  }
})
