const API = '/api';
const STORAGE_KEY = 'ridesharelife.profiles';
const ACTIVE_KEY = 'ridesharelife.activeProfile';
const pages = ['overview', 'find', 'publish', 'requests', 'history'];
let profiles = loadProfiles();
let activeUser = profiles.find(user => String(user.id) === localStorage.getItem(ACTIVE_KEY)) || null;
let latestHistory = null;
let noticeTimer;

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));

function loadProfiles() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'); }
  catch { return []; }
}
function saveProfiles() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(profiles));
  if (activeUser) localStorage.setItem(ACTIVE_KEY, String(activeUser.id));
}
function showNotice(message, kind = 'info') {
  const notice = $('#notice');
  notice.textContent = message;
  notice.className = `notice ${kind}`;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => notice.classList.add('hidden'), 4200);
}
async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { ...(options.body ? {'Content-Type':'application/json'} : {}), ...(options.headers || {}) }
  });
  const text = await response.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!response.ok) throw new Error(body?.error || body?.message || `Request failed (${response.status}).`);
  return body;
}
function setProfile(user) {
  activeUser = user || null;
  if (activeUser) localStorage.setItem(ACTIVE_KEY, String(activeUser.id));
  else localStorage.removeItem(ACTIVE_KEY);
  renderProfiles();
  refreshDashboard();
}
function renderProfiles() {
  const selector = $('#active-user');
  selector.innerHTML = '<option value="">Choose profile</option>' + profiles.map(user => `<option value="${user.id}">${escapeHtml(user.name)}</option>`).join('');
  selector.value = activeUser ? String(activeUser.id) : '';
  $('#avatar').textContent = activeUser?.name?.trim()?.[0]?.toUpperCase() || '?';
  $('#welcome-title').innerHTML = activeUser ? `Good to see you, <em>${escapeHtml(activeUser.name.split(' ')[0])}.</em>` : 'Make your commute <em>better.</em>';
  $('#profile-prompt').classList.toggle('hidden', Boolean(activeUser));
}
function navigate(page) {
  if (!pages.includes(page)) page = 'overview';
  $$('.page').forEach(section => section.classList.toggle('active', section.id === `page-${page}`));
  $$('.nav-link').forEach(button => button.classList.toggle('active', button.dataset.page === page));
  const names = {overview:'Overview', find:'Find a ride', publish:'Publish a ride', requests:'Requests', history:'Ride history'};
  $('#crumb-current').textContent = names[page];
  history.replaceState(null, '', `#${page}`);
  if (page === 'requests' || page === 'history' || page === 'overview') refreshDashboard();
}
function localDateTime(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}
function displayDate(value, options = {weekday:'short', month:'short', day:'numeric', hour:'numeric', minute:'2-digit'}) {
  if (!value) return 'Time not set';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return escapeHtml(value);
  return new Intl.DateTimeFormat(undefined, options).format(date);
}
function showEmpty(container, icon, title, description, extra = '') {
  container.classList.add('empty-state');
  container.innerHTML = `<div class="empty-illustration">${icon}</div><strong>${escapeHtml(title)}</strong><p>${escapeHtml(description)}</p>${extra}`;
}
function rideMarkup(ride) {
  const isOwnRide = activeUser && Number(ride.driver.id) === Number(activeUser.id);
  const outOfSeats = ride.availableSeats < 1;
  const buttonLabel = isOwnRide ? 'Your ride' : outOfSeats ? 'No seats left' : 'Request a seat';
  const disabled = !activeUser || isOwnRide || outOfSeats;
  return `<article class="ride-card">
    <div class="ride-card-top"><div class="driver-chip"><span class="driver-avatar">${escapeHtml(ride.driver.name?.[0]?.toUpperCase() || 'D')}</span><div><span class="driver-name">${escapeHtml(ride.driver.name)}</span><span class="driver-label">Your driver</span></div></div><span class="seat-badge ${outOfSeats?'full':''}">${ride.availableSeats} seat${ride.availableSeats===1?'':'s'} left</span></div>
    <div class="ride-route"><span class="route-place">${escapeHtml(ride.origin)}</span><span class="route-mid">→</span><span class="route-place">${escapeHtml(ride.destination)}</span></div>
    <div class="ride-meta"><span>◷ ${displayDate(ride.departureTime)}</span><span>↗ ${ride.totalSeats} total seats</span></div>
    <button class="button ${disabled?'button-outline':'button-primary'} request-seat-button" data-ride-id="${ride.id}" ${disabled?'disabled':''}>${activeUser?'':'Choose a profile'}${activeUser?buttonLabel:''}</button>
  </article>`;
}
function requestStatus(status) {
  const name = String(status || 'PENDING').toLowerCase();
  return `<span class="status-badge ${name}">${escapeHtml(status || 'PENDING')}</span>`;
}
function renderActivity(history) {
  const items = [
    ...(history.ridesAsDriver || []).map(ride => ({kind:'driver', time:ride.departureTime, ride, status:null})),
    ...(history.ridesAsRider || []).map(request => ({kind:'rider', time:request.ride.departureTime, ride:request.ride, status:request.status}))
  ].sort((a,b) => new Date(a.time) - new Date(b.time)).filter(item => new Date(item.time) >= new Date(Date.now() - 86400000)).slice(0,4);
  const container = $('#overview-activity');
  if (!items.length) {
    showEmpty(container, '↗', 'Your next shared ride starts here', 'Publish a route or search for a ride that fits your plans.', '<button class="button button-outline" data-go="find">Find a ride</button>');
    return;
  }
  container.classList.remove('empty-state');
  container.innerHTML = `<div class="activity-rows">${items.map(item => `<div class="history-item"><div class="history-main"><strong>${escapeHtml(item.ride.origin)} <span class="route-mid">→</span> ${escapeHtml(item.ride.destination)}</strong><small>${item.kind==='driver'?'Driving':'Riding'} · ${displayDate(item.time)}</small></div>${item.status?requestStatus(item.status):'<span class="status-badge">Offering</span>'}</div>`).join('')}</div>`;
}
function renderInbox(history) {
  const requests = history.requestsAsDriver || [];
  const pending = requests.filter(request => request.status === 'PENDING');
  const count = $('#nav-request-count');
  count.textContent = pending.length;
  count.classList.toggle('hidden', !pending.length);
  const container = $('#driver-requests');
  if (!requests.length) {
    showEmpty(container, '✓', 'All caught up', 'New seat requests on your rides will appear here.');
    return;
  }
  const ordered = [...requests].sort((a,b) => new Date(b.requestedAt) - new Date(a.requestedAt));
  container.classList.remove('empty-state');
  container.innerHTML = ordered.map(request => `<article class="request-card">
    <div class="request-person"><span class="driver-avatar">${escapeHtml(request.rider.name?.[0]?.toUpperCase() || 'R')}</span><div><div class="driver-name">${escapeHtml(request.rider.name)}</div><div class="request-time">Requested ${displayDate(request.requestedAt)}</div></div></div>
    <div><div class="request-route">${escapeHtml(request.ride.origin)} <span class="route-mid">→</span> ${escapeHtml(request.ride.destination)}</div><div class="request-time">Ride departs ${displayDate(request.ride.departureTime)}</div></div>
    <div class="request-actions">${request.status === 'PENDING' ? `<button class="button button-small button-approve" data-decision="APPROVED" data-request-id="${request.id}">Approve</button><button class="button button-small button-reject" data-decision="REJECTED" data-request-id="${request.id}">Decline</button>` : requestStatus(request.status)}</div>
  </article>`).join('');
}
function historyItem(title, detail, status = '') {
  return `<div class="history-item"><div class="history-main"><strong>${escapeHtml(title)}</strong><small>${escapeHtml(detail)}</small></div>${status ? requestStatus(status) : ''}</div>`;
}
function renderHistory(history) {
  const driver = $('#history-driver');
  const rider = $('#history-rider');
  const inbox = $('#history-inbox');
  const offers = history.ridesAsDriver || [];
  const riderRequests = history.ridesAsRider || [];
  const driverRequests = history.requestsAsDriver || [];
  driver.innerHTML = offers.length ? offers.map(ride => historyItem(`${ride.origin} → ${ride.destination}`, `${displayDate(ride.departureTime)} · ${ride.availableSeats}/${ride.totalSeats} seats left`, 'Offering')).join('') : '<div class="history-empty">You haven’t published a ride yet.</div>';
  rider.innerHTML = riderRequests.length ? riderRequests.map(request => historyItem(`${request.ride.origin} → ${request.ride.destination}`, `${displayDate(request.ride.departureTime)} · Requested ${displayDate(request.requestedAt,{month:'short',day:'numeric'})}`, request.status)).join('') : '<div class="history-empty">You haven’t requested a ride yet.</div>';
  inbox.innerHTML = driverRequests.length ? driverRequests.map(request => historyItem(`${request.rider.name} · ${request.ride.origin} → ${request.ride.destination}`, `Requested ${displayDate(request.requestedAt,{month:'short',day:'numeric',hour:'numeric',minute:'2-digit'})}`, request.status)).join('') : '<div class="history-empty">No requests on your rides yet.</div>';
}
function renderStats(history) {
  const asDriver = history.ridesAsDriver || [];
  const asRider = history.ridesAsRider || [];
  const received = history.requestsAsDriver || [];
  $('#stat-offers').textContent = asDriver.length;
  $('#stat-requests').textContent = asRider.length + received.length;
  $('#stat-approved').textContent = [...asRider, ...received].filter(request => request.status === 'APPROVED').length;
  $('#stat-pending').textContent = received.filter(request => request.status === 'PENDING').length;
}
async function refreshDashboard() {
  if (!activeUser) {
    latestHistory = null;
    renderStats({});
    renderInbox({});
    renderHistory({});
    renderActivity({});
    return;
  }
  try {
    const history = await api(`/users/${activeUser.id}/history`);
    latestHistory = history;
    renderStats(history);
    renderInbox(history);
    renderHistory(history);
    renderActivity(history);
  } catch (error) {
    showNotice(error.message, 'error');
  }
}

$$('.nav-link').forEach(button => button.addEventListener('click', () => navigate(button.dataset.page)));
$$('[data-go]').forEach(button => button.addEventListener('click', () => navigate(button.dataset.go)));
$('#active-user').addEventListener('change', event => {
  setProfile(profiles.find(user => String(user.id) === event.target.value) || null);
});
$('#add-profile-button').addEventListener('click', () => $('#profile-dialog').showModal());
$('#create-first-profile').addEventListener('click', () => $('#profile-dialog').showModal());
$('#close-profile').addEventListener('click', () => $('#profile-dialog').close());
$('#profile-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const user = await api('/users', {method:'POST', body:JSON.stringify({name:form.get('name').trim(), email:form.get('email').trim()})});
    profiles = profiles.filter(existing => String(existing.id) !== String(user.id));
    profiles.unshift(user);
    saveProfiles();
    $('#profile-dialog').close();
    event.currentTarget.reset();
    setProfile(user);
    showNotice(`Welcome, ${user.name}. Your profile is ready.`, 'success');
  } catch (error) {
    showNotice(error.message, 'error');
  }
});
$('#existing-profile-form').addEventListener('submit', async event => {
  event.preventDefault();
  const userId = new FormData(event.currentTarget).get('userId');
  try {
    const user = await api(`/users/${encodeURIComponent(userId)}`);
    profiles = profiles.filter(existing => String(existing.id) !== String(user.id));
    profiles.unshift(user);
    saveProfiles();
    $('#profile-dialog').close();
    event.currentTarget.reset();
    setProfile(user);
    showNotice(`Welcome back, ${user.name}.`, 'success');
  } catch (error) { showNotice(error.message, 'error'); }
});

$('#publish-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (!activeUser) { showNotice('Create or choose your profile before publishing a ride.', 'error'); $('#profile-dialog').showModal(); return; }
  const form = new FormData(event.currentTarget);
  try {
    const ride = await api('/rides', {method:'POST', body:JSON.stringify({driverId:activeUser.id, origin:form.get('origin').trim(), destination:form.get('destination').trim(), departureTime:form.get('departureTime'), seats:Number(form.get('seats'))})});
    event.currentTarget.reset();
    setDefaultDates();
    showNotice(`Ride published: ${ride.origin} to ${ride.destination}.`, 'success');
    await refreshDashboard();
    navigate('history');
  } catch (error) { showNotice(error.message, 'error'); }
});

$('#search-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const results = $('#search-results');
  results.classList.remove('empty-state');
  results.innerHTML = '<div class="loading-state">Searching available rides…</div>';
  try {
    const params = new URLSearchParams({origin:form.get('origin').trim(), destination:form.get('destination').trim(), from:form.get('from'), to:form.get('to')});
    const rides = await api(`/rides/search?${params}`);
    $('#results-title').textContent = rides.length ? 'Rides that match your route' : 'No rides found just yet';
    $('#results-count').textContent = `${rides.length} ${rides.length === 1 ? 'ride' : 'rides'}`;
    if (!rides.length) showEmpty(results, '⌕', 'No matching rides yet', 'Try a wider departure window or another route.');
    else { results.classList.remove('empty-state'); results.innerHTML = rides.map(rideMarkup).join(''); }
  } catch (error) { showEmpty(results, '!', 'Search could not be completed', error.message); }
});

document.addEventListener('click', async event => {
  const requestButton = event.target.closest('.request-seat-button');
  const decisionButton = event.target.closest('[data-decision]');
  if (requestButton) {
    if (!activeUser) { showNotice('Create or choose your profile before requesting a seat.', 'error'); $('#profile-dialog').showModal(); return; }
    requestButton.disabled = true;
    try {
      await api('/requests', {method:'POST', body:JSON.stringify({riderId:activeUser.id, rideOfferId:Number(requestButton.dataset.rideId)})});
      requestButton.textContent = 'Request sent';
      requestButton.classList.remove('button-primary');
      requestButton.classList.add('button-outline');
      showNotice('Seat request sent to the driver.', 'success');
      await refreshDashboard();
    } catch (error) { requestButton.disabled = false; showNotice(error.message, 'error'); }
  }
  if (decisionButton) {
    if (!activeUser) return;
    decisionButton.disabled = true;
    try {
      await api(`/requests/${decisionButton.dataset.requestId}/decision`, {method:'PUT', body:JSON.stringify({driverId:activeUser.id, status:decisionButton.dataset.decision})});
      showNotice(decisionButton.dataset.decision === 'APPROVED' ? 'Seat request approved.' : 'Seat request declined.', 'success');
      await refreshDashboard();
    } catch (error) { decisionButton.disabled = false; showNotice(error.message, 'error'); }
  }
});

function setDefaultDates() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(8, 30, 0, 0);
  const publishTime = $('#publish-form [name="departureTime"]');
  if (!publishTime.value) publishTime.value = localDateTime(tomorrow);
  const from = $('#search-form [name="from"]');
  const to = $('#search-form [name="to"]');
  if (!from.value) from.value = localDateTime(new Date(tomorrow.getTime() - 60 * 60 * 1000));
  if (!to.value) to.value = localDateTime(new Date(tomorrow.getTime() + 60 * 60 * 1000));
  const minDate = localDateTime(new Date());
  publishTime.min = minDate;
  from.min = minDate;
  to.min = minDate;
}
$('#today-label').textContent = new Intl.DateTimeFormat(undefined,{weekday:'short',month:'short',day:'numeric'}).format(new Date());
renderProfiles();
setDefaultDates();
navigate(location.hash.slice(1) || 'overview');
