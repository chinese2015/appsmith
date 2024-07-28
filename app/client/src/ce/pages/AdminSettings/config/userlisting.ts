import type { AdminConfigType } from "@appsmith/pages/AdminSettings/config/types";
import {
  CategoryType,
  SettingCategories,
  SettingTypes,
} from "@appsmith/pages/AdminSettings/config/types";
import UserRoleAssignment from "../../Upgrade/UserRoleAssignment";
//import { UserRoleAssignment } from "RBAC/UserRoleAssignment";

export const config: AdminConfigType = {
  icon: "user-3-line",
  type: SettingCategories.ACCESS_CONTROL,
  categoryType: CategoryType.ACL,
  controlType: SettingTypes.PAGE,
  component: UserRoleAssignment,
  title: "Access Control",
  canSave: false,
  isFeatureEnabled: true,
} as AdminConfigType;
