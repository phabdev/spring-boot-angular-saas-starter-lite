import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { Project, ProjectInput } from './api.models';

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl + '/projects';
  list() { return this.http.get<Project[]>(this.url); }
  create(input: ProjectInput) { return this.http.post<Project>(this.url, input); }
  update(id: string, input: ProjectInput) { return this.http.put<Project>(this.url + '/' + encodeURIComponent(id), input); }
  delete(id: string) { return this.http.delete<void>(this.url + '/' + encodeURIComponent(id)); }
}
