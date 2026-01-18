import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SystemService {
  private initialized: boolean;
  constructor() {}

  isInitialized(): boolean {
    return this.initialized;
  }

  setInitialized(state: boolean) {
    this.initialized = state;
  }
}
