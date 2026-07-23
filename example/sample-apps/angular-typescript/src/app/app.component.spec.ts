import { AppComponent } from './app.component';

export function appComponentHasTitle(): boolean {
  return new AppComponent().title === 'Angular review sample';
}
