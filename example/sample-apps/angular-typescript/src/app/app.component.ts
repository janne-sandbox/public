import { Component } from '@angular/core';

@Component({
  selector: 'review-sample-root',
  standalone: true,
  template: `
    <main>
      <h1>{{ title }}</h1>
    </main>
  `
})
export class AppComponent {
  title = 'Angular review sample';
}
