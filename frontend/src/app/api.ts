import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AppInfo,
  Dictionaries,
  RallyConfigRequest,
  RallySummary,
  RallyView,
  RecordView,
  ResultRequest,
  SetupStatus,
} from './models';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';
  private readonly options = { withCredentials: true };

  login(password: string): Promise<AppInfo> {
    return firstValueFrom(
      this.http.post<AppInfo>(`${this.base}/auth/login`, { password }, this.options),
    );
  }

  me(): Promise<AppInfo> {
    return firstValueFrom(this.http.get<AppInfo>(`${this.base}/auth/me`, this.options));
  }

  logout(): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${this.base}/auth/logout`, {}, this.options));
  }

  publicApp(): Promise<AppInfo> {
    return firstValueFrom(this.http.get<AppInfo>(`${this.base}/public/app`, this.options));
  }

  publicStatus(): Promise<SetupStatus> {
    return firstValueFrom(this.http.get<SetupStatus>(`${this.base}/public/status`, this.options));
  }

  setup(password: string, confirm: string, logo: File | null): Promise<SetupStatus> {
    const form = new FormData();
    form.append('password', password);
    form.append('confirm', confirm);
    if (logo) {
      form.append('logo', logo);
    }
    return firstValueFrom(
      this.http.post<SetupStatus>(`${this.base}/public/setup`, form, this.options),
    );
  }

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string): Promise<void> {
    return firstValueFrom(
      this.http.put<void>(
        `${this.base}/settings/password`,
        { currentPassword, newPassword, confirmPassword },
        this.options,
      ),
    );
  }

  updateLogo(logo: File): Promise<SetupStatus> {
    const form = new FormData();
    form.append('logo', logo);
    return firstValueFrom(
      this.http.post<SetupStatus>(`${this.base}/settings/logo`, form, this.options),
    );
  }

  rallies(): Promise<RallySummary[]> {
    return firstValueFrom(this.http.get<RallySummary[]>(`${this.base}/rallies`, this.options));
  }

  rally(id: number): Promise<RallyView> {
    return firstValueFrom(this.http.get<RallyView>(`${this.base}/rallies/${id}`, this.options));
  }

  create(request: RallyConfigRequest): Promise<RallyView> {
    return firstValueFrom(
      this.http.post<RallyView>(`${this.base}/rallies`, request, this.options),
    );
  }

  updateConfig(id: number, request: RallyConfigRequest): Promise<RallyView> {
    return firstValueFrom(
      this.http.put<RallyView>(`${this.base}/rallies/${id}/config`, request, this.options),
    );
  }

  setResult(id: number, stageId: number, entrantId: number, request: ResultRequest): Promise<RallyView> {
    return firstValueFrom(
      this.http.put<RallyView>(
        `${this.base}/rallies/${id}/results/${stageId}/${entrantId}`,
        request,
        this.options,
      ),
    );
  }

  retire(id: number, entrantId: number, stageId: number): Promise<RallyView> {
    return firstValueFrom(
      this.http.post<RallyView>(
        `${this.base}/rallies/${id}/entrants/${entrantId}/retire?stageId=${stageId}`,
        {},
        this.options,
      ),
    );
  }

  unretire(id: number, entrantId: number): Promise<RallyView> {
    return firstValueFrom(
      this.http.delete<RallyView>(
        `${this.base}/rallies/${id}/entrants/${entrantId}/retire`,
        this.options,
      ),
    );
  }

  deleteRally(id: number): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.base}/rallies/${id}`, this.options),
    );
  }

  reopenRally(id: number): Promise<RallyView> {
    return firstValueFrom(
      this.http.post<RallyView>(`${this.base}/rallies/${id}/reopen`, {}, this.options),
    );
  }

  records(): Promise<RecordView[]> {
    return firstValueFrom(this.http.get<RecordView[]>(`${this.base}/records`, this.options));
  }

  dictionaries(): Promise<Dictionaries> {
    return firstValueFrom(this.http.get<Dictionaries>(`${this.base}/dictionaries`, this.options));
  }

  publicRally(token: string): Promise<RallyView> {
    return firstValueFrom(
      this.http.get<RallyView>(`${this.base}/public/rallies/${token}`, this.options),
    );
  }

  exportUrl(id: number): string {
    return `${this.base}/rallies/${id}/export`;
  }
}
