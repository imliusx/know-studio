"use client"

import { useState } from "react"
import { type Table } from "@tanstack/react-table"
import { AlertTriangle } from "lucide-react"
import { useTranslation } from "react-i18next"
import { toast } from "sonner"
import { sleep } from "@/lib/utils"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ConfirmDialog } from "@/components/confirm-dialog"

type UserMultiDeleteDialogProps<TData> = {
  open: boolean
  onOpenChange: (open: boolean) => void
  table: Table<TData>
}

const CONFIRM_WORD = "DELETE"

export function UsersMultiDeleteDialog<TData>({
  open,
  onOpenChange,
  table,
}: UserMultiDeleteDialogProps<TData>) {
  const { t } = useTranslation()
  const [value, setValue] = useState("")

  const selectedRows = table.getFilteredSelectedRowModel().rows
  const entityLabel =
    selectedRows.length === 1
      ? t("users.entity.singular")
      : t("users.entity.plural")

  const handleDelete = () => {
    if (value.trim() !== CONFIRM_WORD) {
      toast.error(
        t("users.multiDeleteDialog.typeConfirm", {
          confirmWord: CONFIRM_WORD,
        })
      )
      return
    }

    onOpenChange(false)

    toast.promise(sleep(2000), {
      loading: t("users.multiDeleteDialog.loading"),
      success: () => {
        setValue("")
        table.resetRowSelection()
        return t("users.multiDeleteDialog.success", {
          count: selectedRows.length,
          entity: entityLabel,
        })
      },
      error: t("users.bulk.error"),
    })
  }

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      form="users-multi-delete-form"
      disabled={value.trim() !== CONFIRM_WORD}
      title={
        <span className="text-destructive">
          <AlertTriangle
            className="me-1 inline-block stroke-destructive"
            size={18}
          />{" "}
          {t("users.multiDeleteDialog.title", {
            count: selectedRows.length,
            entity: entityLabel,
          })}
        </span>
      }
      desc={
        <form
          id="users-multi-delete-form"
          onSubmit={(e) => {
            e.preventDefault()
            handleDelete()
          }}
          className="space-y-4"
        >
          <p className="mb-2">{t("users.multiDeleteDialog.description")}</p>

          <Label className="my-4 flex flex-col items-start gap-1.5">
            <span className="">
              {t("users.multiDeleteDialog.confirmLabel", {
                confirmWord: CONFIRM_WORD,
              })}
            </span>
            <Input
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={t("users.multiDeleteDialog.placeholder", {
                confirmWord: CONFIRM_WORD,
              })}
              autoFocus
            />
          </Label>

          <Alert variant="destructive">
            <AlertTitle>{t("users.deleteDialog.warningTitle")}</AlertTitle>
            <AlertDescription>
              {t("users.deleteDialog.warningDescription")}
            </AlertDescription>
          </Alert>
        </form>
      }
      confirmText={t("users.deleteDialog.confirm")}
      destructive
    />
  )
}
