declare module '@angular/core' {
  export interface ComponentMetadata {
    selector: string;
    standalone?: boolean;
    template: string;
  }

  export function Component(metadata: ComponentMetadata): ClassDecorator;
}
