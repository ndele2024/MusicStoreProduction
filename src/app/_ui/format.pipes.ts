import { Pipe, PipeTransform } from '@angular/core';

const COMPACT = new Intl.NumberFormat('fr-FR', { notation: 'compact', maximumFractionDigits: 1 });

/** 1 500 000 devient « 1,5 M » : les compteurs restent lisibles sur une carte étroite. */
@Pipe({ name: 'vues', standalone: true })
export class VuesPipe implements PipeTransform {
  transform(valeur: number | null | undefined): string {
    const nombre = valeur ?? 0;
    return `${COMPACT.format(nombre)} ${nombre > 1 ? 'vues' : 'vue'}`;
  }
}

/** Secondes vers « m:ss ». */
@Pipe({ name: 'duree', standalone: true })
export class DureePipe implements PipeTransform {
  transform(secondes: number | null | undefined): string {
    if (secondes === null || secondes === undefined || !Number.isFinite(secondes) || secondes < 0) {
      return '0:00';
    }
    const minutes = Math.floor(secondes / 60);
    const reste = Math.floor(secondes % 60);
    return `${minutes}:${reste.toString().padStart(2, '0')}`;
  }
}
