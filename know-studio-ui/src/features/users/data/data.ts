import {
  CircleCheck,
  CircleEllipsis,
  CircleMinus,
  CircleX,
  CreditCard,
  Shield,
  UserCheck,
  Users,
  type LucideIcon,
} from "lucide-react"
import { type UserStatus } from "./schema"

export const userStatuses = new Map<
  UserStatus,
  {
    label: string
    labelKey: string
    icon: LucideIcon
    iconClassName?: string
  }
>([
  [
    "active",
    {
      label: "Active",
      labelKey: "users.status.active",
      icon: CircleCheck,
      iconClassName: "fill-primary text-primary-foreground",
    },
  ],
  [
    "inactive",
    {
      label: "Inactive",
      labelKey: "users.status.inactive",
      icon: CircleMinus,
      iconClassName: "fill-muted-foreground text-background",
    },
  ],
  [
    "invited",
    {
      label: "Invited",
      labelKey: "users.status.invited",
      icon: CircleEllipsis,
      iconClassName: "fill-secondary-foreground text-background",
    },
  ],
  [
    "suspended",
    {
      label: "Suspended",
      labelKey: "users.status.suspended",
      icon: CircleX,
      iconClassName: "fill-destructive text-background",
    },
  ],
])

export const roles = [
  {
    label: "Superadmin",
    labelKey: "users.roles.superadmin",
    value: "superadmin",
    icon: Shield,
  },
  {
    label: "Admin",
    labelKey: "users.roles.admin",
    value: "admin",
    icon: UserCheck,
  },
  {
    label: "Manager",
    labelKey: "users.roles.manager",
    value: "manager",
    icon: Users,
  },
  {
    label: "Cashier",
    labelKey: "users.roles.cashier",
    value: "cashier",
    icon: CreditCard,
  },
] as const
