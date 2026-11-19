import { useEffect, useRef, useState } from 'react'
import { notificationRepository } from '../models/notificationRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { useRemoteResource } from './useRemoteResource'

export function useNotificationPreferencesViewModel() {
  const resource=useRemoteResource('/user/notification-preferences/')
  const [draft,setDraft]=useState(null)
  const [busy,setBusy]=useState(false)
  const [error,setError]=useState('')
  const [message,setMessage]=useState('')
  const working=useRef(false)
  useEffect(()=>{setDraft(null);setError('');setMessage('');setBusy(false)},[resource.owner])
  const form=draft?.owner_id===resource.owner?draft:resource.data
  const save=async next=>{
    if(working.current||!next)return
    const owner=currentOwner()
    working.current=true;setBusy(true);setError('');setMessage('')
    try {
      const data=await notificationRepository.savePreferences({version:next.version,rating_changes:next.rating_changes,channels:next.channels})
      if(owner===currentOwner()){setDraft(data);setMessage('Preferencias guardadas.')}
    }catch(failure){if(owner===currentOwner())setError(errorMessage(failure))}
    finally{working.current=false;if(owner===currentOwner())setBusy(false)}
  }
  const change=(channel,value)=>{
    if(!form||working.current)return
    const next=channel==='rating_changes'?{...form,rating_changes:value}:{...form,channels:{...form.channels,[channel]:value}}
    setDraft(next);return save(next)
  }
  return {resource,form,busy,error,message,change,retry:()=>save(form),
    reload:()=>{setDraft(null);setError('');setMessage('');resource.reload()}}
}
