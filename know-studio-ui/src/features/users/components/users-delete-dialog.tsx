"use client"

import { useState } from "react"
import { AlertTriangle } from "lucide-react"
import { useTranslation } from "react-i18next"
import { showSubmittedData } from "@/lib/show-submitted-data"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { roles } from "../data/data"
import { type User } from "../data/schema"

type UserDeleteDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  currentRow: User
}

export function UsersDeleteDialog({
  open,
  onOpenChange,
  currentRow,
}: UserDeleteDialogProps) {
  const { t } = useTranslation()
  const [value, setValue] = useState("")
  const userRole = roles.find((role) => role.value === currentRow.role)
  const roleLabel = userRole ? t(userRole.labelKey) : currentRow.role

  const handleDelete = () => {
    if (value.trim() !== currentRow.username) return

    onOpenChange(false)
    showSubmittedData(currentRow, t("users.deleteDialog.submittedTitle"))
  }

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      form="users-delete-form"
      disabled={value.trim() !== currentRow.username}
      title={
        <span className="text-destructive">
          <AlertTriangle
            className="me-1 inline-block stroke-destructive"
            size={18}
          />{" "}
          {t("users.deleteDialog.title")}
        </span>
      }
      desc={
        <form
          id="users-delete-form"
          onSubmit={(e) => {
            e.preventDefault()
            handleDelete()
          }}
          className="space-y-4"
        >
          <p className="mb-2">
            {t("users.deleteDialog.confirmPrefix")}{" "}
            <span className="font-bold">{currentRow.username}</span>?
            <br />
            {t("users.deleteDialog.descriptionPrefix")}{" "}
            <span className="font-bold">{roleLabel}</span>{" "}
            {t("users.deleteDialog.descriptionSuffix")}
          </p>

          <Label className="my-2">
            {t("users.deleteDialog.username")}
            <Input
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={t("users.deleteDialog.placeholder")}
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
