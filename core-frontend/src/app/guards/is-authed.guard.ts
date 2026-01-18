import { Injectable } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class IsAuthedGuard  {
  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  async canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Promise<boolean | UrlTree> {
    const authed = await this.auth.checkAuthState();
    if (!authed) {
      const prevURL = this.router.routerState.snapshot.url;
      if (!prevURL?.length) {
        this.router.createUrlTree(['/'], { relativeTo: this.route });
      } else {
        this.router.createUrlTree([prevURL]);
      }
    }
    return authed;
  }
}
