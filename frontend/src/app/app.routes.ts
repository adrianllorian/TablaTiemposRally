import { Routes } from '@angular/router';
import { Home } from './home';
import { Viewer } from './viewer';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'ver/:token', component: Viewer },
  { path: '**', redirectTo: '' },
];
