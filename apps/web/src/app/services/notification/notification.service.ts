import {inject, Injectable} from "@angular/core";
import {HubConnection, HubConnectionBuilder} from "@microsoft/signalr";
import {getHubProxyFactory, getReceiverRegister, Disposable} from "../../../notificationservice-client/TypedSignalR.Client";
import {UserService} from "../user/user.service";
import {IPushNotificationHub} from "../../../notificationservice-client/TypedSignalR.Client/NotificationService.Hubs";
import {PushNotificationDto} from "../../../notificationservice-client/NotificationService.Classes.Dto";
import {BehaviorSubject, combineLatestWith, distinctUntilChanged, firstValueFrom, map, Observable, switchMap, take} from "rxjs";
import {ToastService} from "../toast/toast.service";
import {environment} from "../../environment";

interface connectionState {
  connection: HubConnection;
  subscription: Disposable;
  hub: IPushNotificationHub;
}

@Injectable({
  providedIn: "root"
})
export class NotificationService {

  private readonly _hubUrl = `${environment.notificationserviceBasePath}/hubs/push`;
  private readonly _userService = inject(UserService);
  private readonly _toastService = inject(ToastService);

  private _connectionState$ = new BehaviorSubject<connectionState | undefined>(undefined);
  private _notifications$ = new BehaviorSubject<Set<PushNotificationDto>>(new Set());

  constructor() {

    /* handle connection when user changes */
    this._userService.userProfile$.pipe(
      combineLatestWith(this._connectionState$),
      switchMap(async ([profile, connectionState]) => {
        if(profile && !connectionState) {
          return await this.setupConnectionAsync();
        }
        if(!profile && connectionState) {
          await connectionState.connection.stop();
          connectionState.subscription.dispose();
          connectionState.connection.onclose(async () => {
            await connectionState.connection.stop();
            connectionState.subscription.dispose();
            this._connectionState$.next(undefined);
          });
          return undefined;
        }

        return connectionState;
      }),
      distinctUntilChanged((a, b) => a?.connection?.connectionId === b?.connection?.connectionId)
    ).subscribe(connection => this._connectionState$.next(connection));
  }

  public get notifications$(): Observable<Set<PushNotificationDto>> {
    return this._notifications$.asObservable();
  }

  public get unreadCount$(): Observable<number> {
    return this._notifications$.pipe(
      map(notifications => [...notifications.values()].filter(n => !n.isRead).length)
    );
  }

  public async markAllAsRead() {
    const connection = await firstValueFrom(this._connectionState$);
    if(connection === undefined) {
      throw new Error("Not connected");
    }

    const notifications = await firstValueFrom(this._notifications$);
    let count = 0;
    notifications.forEach(n => {
      if(!n.isRead) {
        n.isRead = true;
        count++;
      }
    });

    if(count > 0) {
      await connection.hub.markAllAsRead();
       this._notifications$.next(notifications);
    }
  }

  public async clearHistory() {
    const connection = await firstValueFrom(this._connectionState$);
    if(connection === undefined) {
      throw new Error("Not connected");
    }

    await connection.hub.clearHistory();
    this._notifications$.next(new Set());
  }

  public async clearNotification(notificationId: string) {
    const connection = await firstValueFrom(this._connectionState$);
    if(connection === undefined) {
      throw new Error("Not connected");
    }

    const notifications = await firstValueFrom(this._notifications$);
    const notification = [...notifications.values()].find(n => n.id === notificationId);
    if(!notification) {
      throw new Error("Notification not found");
    }

    await connection.hub.clearNotification(notificationId);
    notifications.delete(notification);
    this._notifications$.next(notifications);
  }

  private async setupConnectionAsync(): Promise<connectionState> {
    const connection = new HubConnectionBuilder()
      .withUrl(this._hubUrl, {
        withCredentials: false,
        accessTokenFactory: async () => (await this._userService.ensureValidToken()) ?? ""
      })
      .build();
    const hubProxy = getHubProxyFactory("IPushNotificationHub")
      .createHubProxy(connection);
    const subscription = getReceiverRegister("IPushNotificationHubReceiver")
      .register(connection, {
        pushNotificationReceived: async event => this.addNotification(event)
      });

    await connection.start();

    const history = await hubProxy.getHistory();
    this._notifications$.next(new Set(history));

    return {
      connection,
      subscription,
      hub: hubProxy
    };
  }

  private addNotification(event: PushNotificationDto) {
    this._toastService.showNotification(event.title, event.notification, event.url ?? undefined);
    this._notifications$.pipe(
      take(1)
    ).subscribe(notifications => {
      notifications.add(event);
      this._notifications$.next(notifications);
    });
  }
}
