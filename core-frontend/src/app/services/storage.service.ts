import { Injectable } from '@angular/core';
import { Preferences, SetOptions, GetResult, KeysResult } from '@capacitor/preferences';
@Injectable({
  providedIn: 'root',
})
export class StorageService {
  constructor() {}

  async getAllKeys(): Promise<string[]> {
    const { keys }: KeysResult = await Preferences.keys();
    return keys;
  }

  async get(key: string): Promise<string | null> {
    const { value }: GetResult = await Preferences.get({ key });
    return value;
  }

  async set(key: string, value: string): Promise<boolean> {
    try {
      const item: SetOptions = { key, value };
      await Preferences.set(item);
      return true;
    } catch (e) {
      return false;
    }
  }

  async remove(key: string): Promise<boolean> {
    try {
      await Preferences.remove({ key });
      return true;
    } catch (e) {
      return false;
    }
  }
}
