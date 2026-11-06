export interface HouseholdMember {
  userId: string;
  role: 'PRIMARY_OWNER' | 'CO_OWNER';
  displayName?: string;
  email?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'REMOVED';
}

export interface Household {
  id: string;
  name: string;
  status: 'ONBOARDING' | 'ACTIVE' | 'FROZEN' | 'ARCHIVED';
  baseCurrencies: string[];
  balancesByCurrency: Record<string, number>;
  ownerId: string;
  members: Record<string, HouseholdMember>;
  memberIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateHouseholdRequest {
  name: string;
  baseCurrencies: string[];
  partnerEmail?: string;
}

export interface UpdateHouseholdRequest {
  name?: string;
  baseCurrencies?: string[];
}

export interface HouseholdInvitation {
  id: string;
  householdId: string;
  householdName: string;
  inviterId: string;
  inviterName: string;
  invitedEmail: string;
  role: 'CO_OWNER';
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export interface InviteCoOwnerRequest {
  invitedEmail: string;
}

