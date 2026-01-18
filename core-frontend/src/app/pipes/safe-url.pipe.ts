import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({
  name: 'safeUrl',
})
export class SafeUrlPipe implements PipeTransform {
  constructor(protected sanitizer: DomSanitizer) {}
  // transform(URLString: string): SafeResourceUrl | null {
  //   const youtubeREGEX =
  //     /^((?:https?:)?\/\/)?((?:www|m)\.)?((?:youtube\.com|youtu.be))(\/(?:[\w\-]+\?v=|embed\/|v\/)?)([\w\-]+)(\S+)?$/;
  //   if (!youtubeREGEX.test(URLString)) {
  //     return null;
  //   }
  //   return this.sanitizer.bypassSecurityTrustResourceUrl(URLString);
  // }
  transform(URLString: string): SafeResourceUrl | null {
    if (!URLString) return null;

    const match = URLString.match(
      /(?:youtube\.com\/(?:watch\?v=|embed\/|v\/|shorts\/)|youtu\.be\/)([\w\-]{11})/
    );

    if (!match || !match[1]) return null;

    const videoId = match[1];
    const embedUrl = `https://www.youtube.com/embed/${videoId}`;

    return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
  }
}
