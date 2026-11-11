import {BaseWorkflow} from "../../../models/base.workflow";
import SELECTORS_NOTIFICATIONS from "./notifications.selectors";
import notificationsPo from "./notifications.po";

export class NotificationsWorkflow extends BaseWorkflow {
  constructor() {
    super(SELECTORS_NOTIFICATIONS);
  }

  visitNotificationsPage() {
    notificationsPo.visitPageUrl();
  }
}
