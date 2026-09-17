///ce service affiche les notifications en utilisant le composant SnackBar de angular material

import {inject, Injectable} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  //injection du composant MatSnackBar
  private _snackBar = inject(MatSnackBar);
  constructor() { }

  ///Affiche la SnackBar pendant 3 secondes
  ///position verticale:en haut(top)  position horizontale:center
  show(msg:string) {
    this._snackBar.open(msg, "",{
      duration: 3000,
      horizontalPosition:"center",
      verticalPosition:"top",
    });
  }
}
