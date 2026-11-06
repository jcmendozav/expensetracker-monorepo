export interface AppNotification {
  id: string;
  householdId: string;
  recipientId: string;
  actorId: string;
  budgetId?: string;
  budgetTitle?: string;
  invitationId?: string;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  expiresAt?: string;
  createdAt: string;
}

export interface NotificationFeedResponse {
  items: AppNotification[];
  nextCursor?: string;
}

