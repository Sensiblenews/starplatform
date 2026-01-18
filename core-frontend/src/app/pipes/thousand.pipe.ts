import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
	name: 'thousand'
})
export class ThousandPipe implements PipeTransform {

	transform(value: any, args?: any): any {
		const target = String(value).match(/-?\d+/g);
		const toNumber = Number(target ? target[0] : 0);
		if (isNaN(toNumber) || toNumber === 0) { return 0; }
		const replaced = String(toNumber).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return replaced.length ? replaced : 0;
	}

}
