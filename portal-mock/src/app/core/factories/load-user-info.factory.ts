import { lastValueFrom } from "rxjs";
import {SessionService} from "../services/session.service";

export function loadUserInfos(sessionService: SessionService) : () => Promise<any> {
  return () => lastValueFrom(sessionService.loadUserInfos());
}
