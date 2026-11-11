/* eslint-disable @typescript-eslint/member-ordering -- injected dependencies initialize fields below */
import {inject, Injectable} from "@angular/core";
import {BehaviorSubject, distinctUntilChanged, firstValueFrom, map} from "rxjs";
import {NotificationHistoryEndpointService, WebSocketTicketEndpointService} from "../../../api-client";
import {UserService} from "../user/user.service";
import {environment} from "../../environment";

interface DisplayNotification {
  id: string;
  title: string;
  notification: string;
  url?: string;
  time: string;
  isRead: boolean;
}

@Injectable({providedIn: "root"})
export class NotificationService {
  private readonly history = inject(NotificationHistoryEndpointService);
  private readonly tickets = inject(WebSocketTicketEndpointService);
  private readonly user = inject(UserService);
  private readonly notifications = new BehaviorSubject<Set<DisplayNotification>>(new Set());
  private socket?: WebSocket;
  private retry?: ReturnType<typeof setTimeout>;
  private active = false;
  private generation = 0;
  private marking = false;

  readonly notifications$ = this.notifications.asObservable();
  readonly unreadCount$ = this.notifications$.pipe(map(items => [...items].filter(item => !item.isRead).length));

  constructor() {
    this.user.userProfile$.pipe(map(profile => profile !== null), distinctUntilChanged()).subscribe(active => {
      this.active = active;
      this.generation++;
      clearTimeout(this.retry);
      this.socket?.close();
      this.socket = undefined;
      if (active) {void this.connect(this.generation);} else {this.notifications.next(new Set());}
    });
  }

  private async refresh(generation = this.generation): Promise<void> {
    const page = await firstValueFrom(this.history.list(0, 100));
    if (!this.active || generation !== this.generation) {return;}
    this.notifications.next(new Set((page.content ?? []).filter(item => item.id && item.createdAt).map(item => ({
      id: item.id ?? "", title: item.title ?? "Notification", notification: item.body ?? "",
      url: item.linkTarget, time: item.createdAt ?? "", isRead: Boolean(item.readAt)
    }))));
  }

  private async connect(generation: number): Promise<void> {
    try {
      const response = await firstValueFrom(this.tickets.issue());
      if (!this.active || generation !== this.generation || !response.ticket) {return;}
      const ticket = response.ticket;
      const url = new URL("/api/v1/realtime/ws", environment.apiBasePath);
      url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
      const socket = new WebSocket(url);
      this.socket = socket;
      socket.onopen = () => socket.send(ticket);
      socket.onmessage = event => {
        try {
          const message = JSON.parse(event.data) as {type?: string};
          if (message.type === "ready" || message.type === "notifications.changed") {void this.refresh(generation).catch(() => undefined);}
        } catch { /* Ignore messages outside the realtime protocol. */ }
      };
      socket.onerror = () => socket.close();
      socket.onclose = () => {
        if (this.active && generation === this.generation) {this.retry = setTimeout(() => void this.connect(generation), 3000);}
      };
    } catch {
      if (this.active && generation === this.generation) {this.retry = setTimeout(() => void this.connect(generation), 3000);}
    }
  }

  async markAllAsRead(): Promise<void> {
    if (this.marking || ![...this.notifications.value].some(item => !item.isRead)) {return;}
    this.marking = true;
    try { await firstValueFrom(this.history.markAllRead()); await this.refresh(); } finally { this.marking = false; }
  }
  async clearHistory(): Promise<void> { await firstValueFrom(this.history.clear()); await this.refresh(); }
  async clearNotification(id: string): Promise<void> { await firstValueFrom(this.history._delete(id)); await this.refresh(); }
}
