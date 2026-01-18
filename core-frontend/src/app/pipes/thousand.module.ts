import { NgModule } from '@angular/core';
import { ThousandPipe } from './thousand.pipe';

@NgModule({
	declarations: [ThousandPipe],
	exports: [ThousandPipe]
})
export class ThousandModule {}
