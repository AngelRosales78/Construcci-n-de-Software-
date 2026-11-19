import { useEffect, useRef, useState } from 'react'
import { notificationRepository } from '../models/notificationRepository'
import { currentOwner, errorMessage } from '../models/sprint3Repository'
import { useRemoteResource } from './useRemoteResource'

export function useNotificationsViewModel() {
  const [page,setPage]=useState({number:1,anchor:null})
  const resource=useRemoteResource(`/notifications/?page=${page.number}${page.anchor===null?'':`&through_id=${page.anchor}`}`,true,page.number===1&&page.anchor===null?30000:0)
  const [busy,setBusy]=useState(false)
  const [error,setError]=useState('')
  const working=useRef(false)
  useEffect(()=>notificationRepository.subscribe(resource.reload),[resource.reload])
  useEffect(()=>{setPage({number:1,anchor:null});setError('');setBusy(false)},[resource.owner])
  const mark=async all=>{
    if(working.current||!resource.data)return
    const owner=currentOwner()
    working.current=true;setBusy(true);setError('')
    try { await notificationRepository.markRead(typeof all==='number'?{ids:[all]}:{all:true,through_id:resource.data.read_through}) }
    catch(failure){if(owner===currentOwner())setError(errorMessage(failure))}
    finally{working.current=false;if(owner===currentOwner())setBusy(false)}
  }
  return {resource,page:page.number,busy,error,mark,
    refresh:()=>{setPage({number:1,anchor:null});resource.reload()},
    go:direction=>setPage(old=>({number:Math.max(1,old.number+direction),anchor:resource.data?.read_through??old.anchor}))}
}
