/**
 * Authored examples only. This interface is deliberately separate from any future engine.
 * @typedef {'link'|'message'|'screenshot'|'document'} ExampleKind
 * @typedef {{kind:ExampleKind, title:string, input:string, quote:string, evidence:string, explanation:string, unknown:string, next:string, mode:string}} ProductExample
 */
/** @type {ReadonlyArray<Readonly<ProductExample>>} */
export const productExamples = Object.freeze([
  {kind:'link', title:'A familiar name. A different host.', input:'Your Bank\nhttps://your-bank.support-check.example/login', quote:'support-check.example', evidence:'Read the actual destination', explanation:'The familiar name is a subdomain. It does not establish who controls support-check.example.', unknown:'Ownership and live reputation have not been checked. This example never opens the URL.', next:'Use your bank’s known app or type its official address yourself.', mode:'Authored link example. No network lookup.'},
  {kind:'message', title:'An offer with a condition.', input:'Part-time task offer. Join our group to start. Pay a fee to release your earnings.', quote:'Pay a fee to release your earnings.', evidence:'Upfront payment pressure', explanation:'The payment is made a condition of receiving promised income. Pause before sending money.', unknown:'The sender’s identity and the promised earnings remain unverified.', next:'Pause the payment. Verify the employer independently.', mode:'Authored example. No detection engine ran.'},
  {kind:'screenshot', title:'Read the request behind the image.', input:'ACCOUNT NOTICE\nYour account will be blocked today.\nShare your OTP to continue.', quote:'Share your OTP', evidence:'A secret is requested', explanation:'An OTP can authorise account access or a transaction. A screenshot does not make the request trustworthy.', unknown:'OCR, image authenticity and sender verification are not implemented in this frontend.', next:'Do not share the OTP. Contact the organisation through a known channel.', mode:'Authored screenshot text. Not OCR output.'},
  {kind:'document', title:'Readable is not verified.', input:'EXAMPLE TICKET\nPassenger: Asha Demo\nBooking ID: DEMO-4821\nBooking status: unverified', quote:'Booking status: unverified', evidence:'Issuer confirmation needed', explanation:'Readable fields do not prove a valid booking. Missing signatures alone do not establish forgery.', unknown:'No signature, issuer database or live booking check has run.', next:'Verify the booking directly with the operator through its official channel.', mode:'Authored document specimen. Not a file scan.'}
]);
/** @param {ExampleKind|string} kind */
export function exampleFor(kind) { return productExamples.find(example => example.kind === kind) ?? null; }
/** @param {number} position */
export function storyProgress(position) { return Math.max(0, Math.min(1, Number.isFinite(position) ? position : 0)); }
