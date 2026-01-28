export type ApiResp<T> = {
  code: number
  message: string
  data: T
}

export type Operator = {
  id: number
  username: string
  name: string
  status: 'ENABLED' | 'DISABLED' | string
  role: 'ADMIN' | 'CASHIER' | string
}

export type DiningTable = {
  id: number
  code: string
  type: 'HALL' | 'ROOM' | string
  capacity: number
  status: 'FREE' | 'OCCUPIED' | string
  currentOrderId?: number | null
}

export type DishCategory = {
  id: number
  name: string
  sort: number
  status: 'ENABLED' | 'DISABLED' | string
}

export type Dish = {
  id: number
  name: string
  categoryId: number
  price: number
  status: 'ON' | 'OFF' | string
}

export type DiscountRule = {
  id: number
  name: string
  type: 'FIXED_RATE' | 'FULL_REDUCTION' | 'MEMBER' | string
  paramsJson: string
  enabled: 0 | 1 | number
  priority: number
}

export type ConsumeOrder = {
  id: number
  orderNo: string
  tableId: number
  operatorId: number
  status: 'OPEN' | 'CLOSED' | 'CANCELLED' | string
  openedAt: string
  closedAt?: string | null
  amountBeforeDiscount: number
  discountRuleId?: number | null
  discountAmount: number
  discountReason?: string | null
  amountAfterDiscount: number
}

export type OrderItem = {
  id: number
  orderId: number
  dishId: number
  dishNameSnapshot: string
  unitPriceSnapshot: number
  quantity: number
  lineAmount: number
}

export type Payment = {
  id: number
  orderId: number
  method: string
  payAmount: number
  changeAmount: number
  paidAt: string
}

