import type { HttpHeaders } from '@angular/common/http';

interface HeaderOptions {
  [key: string]: string | string[];
  authorization?: string;
  app_authorization?: string;
}

interface CreateHeaderOptions {
  contentType?: string;
  needToken?: boolean;
}

interface RequestHeader {
  headers: HttpHeaders;
}

export type { HeaderOptions, CreateHeaderOptions, RequestHeader };
