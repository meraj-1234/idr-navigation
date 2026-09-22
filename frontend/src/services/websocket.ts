import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { NavigationState, SystemEvent } from '../types/navigation';

export interface WebSocketCallbacks {
  onTelemetry: (state: NavigationState) => void;
  onEvent: (event: SystemEvent) => void;
  onStatusChange: (connected: boolean) => void;
}

export class TelemetryWebSocket {
  private client: Client | null = null;
  private callbacks: WebSocketCallbacks;
  private isExplicitlyClosed = false;

  constructor(callbacks: WebSocketCallbacks) {
    this.callbacks = callbacks;
  }

  public connect(): void {
    this.isExplicitlyClosed = false;

    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.host;
    const socketUrl = `${protocol}//${host}/ws-telemetry`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: () => {}, // silent debug
      onConnect: () => {
        this.callbacks.onStatusChange(true);

        // Subscribe to live navigation telemetry
        this.client?.subscribe('/topic/telemetry', (message) => {
          try {
            const data: NavigationState = JSON.parse(message.body);
            this.callbacks.onTelemetry(data);
          } catch (e) {
            console.error('Failed to parse telemetry frame', e);
          }
        });

        // Subscribe to system events
        this.client?.subscribe('/topic/events', (message) => {
          try {
            const event: SystemEvent = JSON.parse(message.body);
            this.callbacks.onEvent(event);
          } catch (e) {
            console.error('Failed to parse event frame', e);
          }
        });
      },
      onDisconnect: () => {
        this.callbacks.onStatusChange(false);
      },
      onStompError: (frame) => {
        console.warn('STOMP broker error', frame.headers['message']);
        this.callbacks.onStatusChange(false);
      },
      onWebSocketClose: () => {
        if (!this.isExplicitlyClosed) {
          this.callbacks.onStatusChange(false);
        }
      },
    });

    this.client.activate();
  }

  public disconnect(): void {
    this.isExplicitlyClosed = true;
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.callbacks.onStatusChange(false);
  }
}
