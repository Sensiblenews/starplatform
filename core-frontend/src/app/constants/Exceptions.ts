class ManualError extends Error {
  public type: string;
  constructor(type, msg) {
    super(msg);
    this.type = type;
  }
}

export { ManualError };
