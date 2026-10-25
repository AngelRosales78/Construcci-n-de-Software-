import { useSyncExternalStore } from 'react'
import { authRepository } from '../models/authRepository'

export function useRouteSessionViewModel() {
  return useSyncExternalStore(authRepository.subscribeSession, authRepository.getSessionIdentity, () => '')
}
