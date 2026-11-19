import { currentOwner, sprint3Repository } from './sprint3Repository'

export const notificationRepository = {
  async markRead(data) {
    const owner=currentOwner()
    const result=await sprint3Repository.send('post','/notifications/mark-read/',data)
    if(owner===currentOwner()) window.dispatchEvent(new Event('sp500-notifications-changed'))
    return result
  },
  savePreferences:data=>sprint3Repository.send('put','/user/notification-preferences/',data),
  subscribe(listener) {
    window.addEventListener('sp500-notifications-changed',listener)
    return ()=>window.removeEventListener('sp500-notifications-changed',listener)
  },
}
