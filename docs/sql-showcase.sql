-- 1) Conteo de pagos por estado y monto agregado.
SELECT
    status,
    COUNT(*) AS total_payments,
    SUM(total_amount) AS total_amount
FROM payment_saga
GROUP BY status
ORDER BY total_payments DESC, total_amount DESC;

-- 2) Trazabilidad completa de una saga con sus pasos e idempotencia.
SELECT
    ps.payment_id,
    ps.order_id,
    ps.customer_id,
    ps.status,
    ps.current_step,
    pst.step_sequence,
    pst.step_name,
    pst.direction,
    pst.status AS step_status,
    pst.error_code,
    ir.request_id,
    ir.http_status
FROM payment_saga ps
LEFT JOIN payment_saga_step pst
       ON pst.payment_id = ps.payment_id
LEFT JOIN idempotency_request ir
       ON ir.payment_id = ps.payment_id
WHERE ps.payment_id = 'REPLACE_WITH_PAYMENT_ID'
ORDER BY pst.step_sequence, pst.direction;

-- 3) Pagos fallidos que ya iniciaron compensación.
SELECT
    ps.payment_id,
    ps.order_id,
    ps.failure_reason,
    ps.compensation_reason,
    MAX(pst.finished_at) AS last_step_finished_at
FROM payment_saga ps
JOIN payment_saga_step pst
  ON pst.payment_id = ps.payment_id
WHERE ps.status IN ('FAILED', 'COMPENSATING', 'COMPENSATED')
GROUP BY ps.payment_id, ps.order_id, ps.failure_reason, ps.compensation_reason
ORDER BY last_step_finished_at DESC;

-- 4) Duración promedio por paso de la saga.
SELECT
    step_name,
    direction,
    AVG(DATEDIFF('SECOND', started_at, finished_at)) AS avg_duration_seconds,
    COUNT(*) AS executions
FROM payment_saga_step
WHERE started_at IS NOT NULL
  AND finished_at IS NOT NULL
GROUP BY step_name, direction
ORDER BY avg_duration_seconds DESC;

-- 5) Detección de reintentos idempotentes por recurso.
SELECT
    resource_type,
    COUNT(*) AS total_requests,
    COUNT(DISTINCT request_hash) AS distinct_payloads,
    COUNT(DISTINCT payment_id) AS affected_payments
FROM idempotency_request
GROUP BY resource_type
ORDER BY total_requests DESC;

-- 6) Top clientes por monto procesado en sagas completadas.
SELECT
    customer_id,
    COUNT(*) AS completed_sagas,
    SUM(total_amount) AS total_processed
FROM payment_saga
WHERE status = 'COMPLETED'
GROUP BY customer_id
HAVING SUM(total_amount) > 0
ORDER BY total_processed DESC, completed_sagas DESC;
